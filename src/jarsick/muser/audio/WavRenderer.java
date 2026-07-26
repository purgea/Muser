/******************************************************************************
 * Copyright (c) 2022, Daniele Aurigemma
 * All rights reserved.
 *
 * Part of the Muser project github: https://github.com/Jarsick/Muser
 */

package jarsick.muser.audio;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

/**
 * Offline MIDI renderer backed by the JDK software synthesizer and an external
 * SoundFont. The executable JAR exports the required JDK synth package through
 * its manifest; reflection keeps project compilation independent of internal
 * JDK APIs.
 */
public final class WavRenderer {
	private static final float SAMPLE_RATE = 48_000f;
	private static final double RELEASE_TAIL_SECONDS = 4.0;
	private static final int DEFAULT_MPQ = 500_000;

	private WavRenderer() {
	}

	public static void render(Sequence sequence, File soundFontFile, File outputFile)
			throws IOException, MidiUnavailableException {
		Soundbank soundbank;
		try {
			soundbank = MidiSystem.getSoundbank(soundFontFile);
		} catch(InvalidMidiDataException e) {
			throw new IOException("Invalid SoundFont: " + soundFontFile.getAbsolutePath(), e);
		}
		if(soundbank == null) {
			throw new IOException("Unsupported SoundFont: " + soundFontFile.getAbsolutePath());
		}

		AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 2, true, false);
		Synthesizer synthesizer = MidiSystem.getSynthesizer();
		try {
			AudioInputStream synthStream = openStream(synthesizer, format);
			if(!synthesizer.loadAllInstruments(soundbank)) {
				throw new IOException("The synthesizer could not load SC-55.sf2");
			}

			long sequenceMicros = schedule(sequence, synthesizer.getReceiver());
			long frameCount = (long)Math.ceil(
					(sequenceMicros / 1_000_000.0 + RELEASE_TAIL_SECONDS) * format.getFrameRate()
					);
			try(AudioInputStream rendered = new AudioInputStream(synthStream, format, frameCount)) {
				AudioSystem.write(rendered, AudioFileFormat.Type.WAVE, outputFile);
			}
		} finally {
			synthesizer.close();
		}
	}

	private static AudioInputStream openStream(Synthesizer synthesizer, AudioFormat format)
			throws IOException, MidiUnavailableException {
		try {
			Method openStream = synthesizer.getClass().getMethod(
					"openStream",
					AudioFormat.class,
					Map.class
					);
			Object stream = openStream.invoke(synthesizer, format, new HashMap<String, Object>());
			return (AudioInputStream)stream;
		} catch(NoSuchMethodException | IllegalAccessException e) {
			throw new IOException(
					"The installed Java runtime does not expose its offline audio synthesizer",
					e
					);
		} catch(InvocationTargetException e) {
			Throwable cause = e.getCause();
			if(cause instanceof MidiUnavailableException midiException) {
				throw midiException;
			}
			throw new IOException("Cannot open the offline audio synthesizer", cause);
		}
	}

	private static long schedule(Sequence sequence, Receiver receiver) {
		List<MidiEvent> events = new ArrayList<>();
		for(var track : sequence.getTracks()) {
			for(int i = 0; i < track.size(); i++) {
				events.add(track.get(i));
			}
		}
		events.sort(Comparator.comparingLong(MidiEvent::getTick));

		long lastTick = 0;
		long elapsedMicros = 0;
		int microsecondsPerQuarter = DEFAULT_MPQ;
		for(MidiEvent event : events) {
			long tick = event.getTick();
			elapsedMicros += ticksToMicroseconds(
					sequence,
					tick - lastTick,
					microsecondsPerQuarter
					);
			lastTick = tick;

			MidiMessage message = event.getMessage();
			if(isTempoMessage(message) && sequence.getDivisionType() == Sequence.PPQ) {
				microsecondsPerQuarter = readTempo((MetaMessage)message);
			} else if(!(message instanceof MetaMessage)) {
				receiver.send(message, elapsedMicros);
			}
		}
		return elapsedMicros;
	}

	private static long ticksToMicroseconds(Sequence sequence, long ticks, int microsecondsPerQuarter) {
		if(sequence.getDivisionType() == Sequence.PPQ) {
			return ticks * microsecondsPerQuarter / sequence.getResolution();
		}
		double ticksPerSecond = sequence.getDivisionType() * sequence.getResolution();
		return (long)(ticks * 1_000_000.0 / ticksPerSecond);
	}

	private static boolean isTempoMessage(MidiMessage message) {
		return message instanceof MetaMessage meta
				&& meta.getType() == 0x51
				&& meta.getData().length == 3;
	}

	private static int readTempo(MetaMessage message) {
		byte[] data = message.getData();
		return (data[0] & 0xff) << 16 | (data[1] & 0xff) << 8 | data[2] & 0xff;
	}
}
