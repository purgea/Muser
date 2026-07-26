/******************************************************************************
 * Copyright (c) 2022, Daniele Aurigemma
 * All rights reserved.
 * 
 * Part of the Muser project github: https://github.com/Jarsick/Muser
 */

package jarsick.muser.gui;
import java.io.File;
import java.io.IOException;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileFilter;

import jarsick.muser.audio.Audio;
import jarsick.muser.audio.WavRenderer;
import jarsick.muser.generator.SongGeneratorSettings;
import jarsick.muser.midi.MIDI;
import jarsick.muser.midi.SequenceExporter;

public class MuserGUIController {
	private Sequence currentSequence;
	private JFrame frame;
	private SongGeneratorSettings settings;
	private Sequencer sequencer;


	public MuserGUIController(JFrame frame) {
		this.frame = frame;
		this.settings = new SongGeneratorSettings().randomize();
		try {
			this.sequencer = MidiSystem.getSequencer();
			sequencer.open();
		} catch (MidiUnavailableException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this.frame, "MIDI player not available!");
		}
	}

	public SongGeneratorSettings getSettings() {
		return this.settings;
	}

	public void generateSong() {
		this.currentSequence = MIDI.createSequence(settings);
	}


	public void playGenertedSong() {
		if(sequencer == null) return;
		try {
			sequencer.stop();
			sequencer.setSequence(this.currentSequence);
			sequencer.start();
		} catch (InvalidMidiDataException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this.frame, "Cannot Play the MIDI file");
		}
	}

	public void saveMIDI() {
		if(!canSave()) {
			return;
		}
		var selectedFile = chooseOutputFile("MIDI file", Audio.MIDI_EXTENSION);
		if(selectedFile != null) {
			this.saveMIDI(selectedFile);
		}
	}

	public void saveWAV(String soundFontFileName) {
		if(!canSave()) {
			return;
		}
		File soundFont = findSoundFont(soundFontFileName);
		if(soundFont == null) {
			JOptionPane.showMessageDialog(
					this.frame,
					soundFontFileName
							+ " was not found. Put it beside Muser or in the project directory."
					);
			return;
		}
		var selectedFile = chooseOutputFile("WAV audio file", Audio.WAV_EXTENSION);
		if(selectedFile == null) {
			return;
		}
		File outputFile = new File(getOutputPath(selectedFile, Audio.WAV_EXTENSION));
		renderWAV(soundFont, outputFile);
	}

	private boolean canSave() {
		if(currentSequence == null) {
			JOptionPane.showMessageDialog(this.frame, "You must generate a song before saving");
			return false;
		}
		return true;
	}

	private File chooseOutputFile(String description, String extension) {
		var fc = new JFileChooser();
		FileFilter filter = new FileFilter() {
			@Override
			public boolean accept(File f) {
				return f.isDirectory()
						|| f.getName().toLowerCase().endsWith(extension);
			}

			@Override
			public String getDescription() {
				return description + " (*" + extension + ")";
			}
		};
		fc.setAcceptAllFileFilterUsed(false);
		fc.setFileFilter(filter);
		int returnVal = fc.showSaveDialog(this.frame);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			return fc.getSelectedFile();
		}
		return null;
	}

	private String getOutputPath(File file, String extension) {
		var absolutePath = file.getAbsolutePath();
		if(!absolutePath.endsWith(extension)){
			absolutePath += extension;
		}
		return absolutePath;
	}
	
	

	private void saveMIDI(File file) {
		exportFile(file, Audio.MIDI_EXTENSION, MIDI::exportMIDI);
	}
	
	private void exportFile(File file, String extension, SequenceExporter exporter) {
		String filePath = this.getOutputPath(file, extension);
		try {
			exporter.export(currentSequence, new File(filePath));
			JOptionPane.showMessageDialog(this.frame,"File exported");
		} catch (IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this.frame,"Cannot export file");
		}
	}

	private void renderWAV(File soundFont, File outputFile) {
		var worker = new SwingWorker<Void, Void>() {
			private Exception failure;

			@Override
			protected Void doInBackground() {
				File temporaryMidi = null;
				try {
					temporaryMidi = File.createTempFile("muser-", Audio.MIDI_EXTENSION);
					MIDI.exportMIDI(currentSequence, temporaryMidi);
					Sequence temporarySequence = MidiSystem.getSequence(temporaryMidi);
					WavRenderer.render(temporarySequence, soundFont, outputFile);
				} catch(Exception e) {
					failure = e;
				} finally {
					if(temporaryMidi != null && !temporaryMidi.delete()) {
						temporaryMidi.deleteOnExit();
					}
				}
				return null;
			}

			@Override
			protected void done() {
				if(failure == null) {
					JOptionPane.showMessageDialog(frame, "WAV file exported");
				} else {
					failure.printStackTrace();
					JOptionPane.showMessageDialog(
							frame,
							"Cannot export WAV: " + failure.getMessage()
							);
				}
			}
		};
		worker.execute();
		JOptionPane.showMessageDialog(
				this.frame,
				"Rendering WAV with " + soundFont.getName() + ". This may take a moment."
				);
	}

	private File findSoundFont(String soundFontFileName) {
		var candidates = new java.util.ArrayList<File>();
		candidates.add(new File(soundFontFileName));
		try {
			File application = new File(
					MuserGUIController.class.getProtectionDomain()
							.getCodeSource().getLocation().toURI()
					);
			File applicationDirectory = application.isDirectory()
					? application
					: application.getParentFile();
			if(applicationDirectory != null) {
				candidates.add(new File(applicationDirectory, soundFontFileName));
				File parent = applicationDirectory.getParentFile();
				if(parent != null) {
					candidates.add(new File(parent, soundFontFileName));
				}
			}
		} catch(Exception ignored) {
			// The working-directory candidate remains available.
		}
		for(File candidate : candidates) {
			if(candidate.isFile() && candidate.canRead()) {
				return candidate;
			}
		}
		return null;
	}

	public void stopSong() {
		if(this.sequencer != null && this.sequencer.isOpen()) {
			this.sequencer.stop();
		}
	}
}
