/******************************************************************************
 * Copyright (c) 2022, Daniele Aurigemma
 * All rights reserved.
 * 
 * Part of the Muser project github: https://github.com/Jarsick/Muser
 */

package jarsick.muser.generator;

public interface InstrumentNames {

	// General MIDI program numbers are one-based here; InstrumentName converts
	// them to the zero-based values used by JFugue.
	int NYLON_GUITAR_PROGRAM = 25;
	int CHOIR_AAHS_PROGRAM = 53;
	// This is an internal layered preset selector, deliberately outside the
	// 1-128 General MIDI program range.
	int DARK_CHOIR_SELECTOR = 129;
	int SYNTH_VOICE_PROGRAM = 55;
	
	InstrumentName[] MELODY = {
			new InstrumentName("Piano", 1),
			new InstrumentName("Harpsichord", 7),
			new InstrumentName("Celesta", 9),
			new InstrumentName("Tubular Bells", 15),
			new InstrumentName("Dulcimer", 16),
			new InstrumentName("Electric Piano", 5),
			new InstrumentName("Rock Organ", 18),
			new InstrumentName("Church Organ", 19),
			new InstrumentName("Accordion", 21),
			new InstrumentName("Lute", NYLON_GUITAR_PROGRAM),
			new InstrumentName("Orchestral Harp", 47),
			new InstrumentName("Timpani", 48),
			new InstrumentName("Tremolo Strings", 45),
			new InstrumentName("Pizzicato Strings", 46),
			new InstrumentName("Clarinet", 72),
			new InstrumentName("Oboe", 69),
			new InstrumentName("English Horn", 70),
			new InstrumentName("Bassoon", 71),
			new InstrumentName("Flute", 74),
			new InstrumentName("Recorder", 75),
			new InstrumentName("Trumpet", 57),
			new InstrumentName("French Horn", 61),
			new InstrumentName("Violin", 41),
			new InstrumentName("Cello", 43),
			new InstrumentName("Contrabass", 44),
			new InstrumentName("Alto Sax", 66),
			new InstrumentName("Tenor Sax", 67),
			new InstrumentName("Acoustic Guitar", NYLON_GUITAR_PROGRAM),
			new InstrumentName("Electric Guitar", 30),
			new InstrumentName("Bag Pipe", 110),
			new InstrumentName("Shanai", 112),
			new InstrumentName("Crystal", 99),
			new InstrumentName("Brightness", 101),
			new InstrumentName("Goblins", 102),
			new InstrumentName("Lead", 81),
	};
	
	InstrumentName[] BASS = {
			new InstrumentName("Acoustic Bass", 33),
			new InstrumentName("Electric Bass", 34),
			new InstrumentName("Slap Bass", 36),
			new InstrumentName("Synth Bass", 39),
			new InstrumentName("Cello", 43),
			new InstrumentName("Contrabass", 44),
			new InstrumentName("Timpani", 48),
			new InstrumentName("Trombone", 58),
			new InstrumentName("Tuba", 59),
			new InstrumentName("Bassoon", 71),
	};
	
	InstrumentName[] CHORDS = {
			new InstrumentName("Choir Aahs", CHOIR_AAHS_PROGRAM),
			new InstrumentName("Choir Oohs", 54),
			new InstrumentName("Dark Choir (Layered)", DARK_CHOIR_SELECTOR),
			new InstrumentName("Synth Voice", SYNTH_VOICE_PROGRAM),
			new InstrumentName("Brass", 62),
			new InstrumentName("French Horn", 61),
			new InstrumentName("Strings", 49),
			new InstrumentName("Tremolo Strings", 45),
			new InstrumentName("Pizzicato Strings", 46),
			new InstrumentName("Orchestral Harp", 47),
			new InstrumentName("String Ensemble 2", 50),
			new InstrumentName("Pad", 89),
			new InstrumentName("Fantasia", 89),
			new InstrumentName("Halo Pad", 95),
			new InstrumentName("Sweep Pad", 96),
			new InstrumentName("Soundtrack", 98),
			new InstrumentName("Crystal", 99),
			new InstrumentName("Atmosphere", 100),
			new InstrumentName("Brightness", 101),
			new InstrumentName("Goblins", 102),
			new InstrumentName("Piano", 1),
			new InstrumentName("Harpsichord", 7),
			new InstrumentName("Dulcimer", 16),
			new InstrumentName("Electric Piano", 5),
			new InstrumentName("Rock Organ", 18),
			new InstrumentName("Church Organ", 19),
			new InstrumentName("Lute", NYLON_GUITAR_PROGRAM),
			new InstrumentName("Acoustic Guitar", NYLON_GUITAR_PROGRAM),
			new InstrumentName("Electric Guitar", 30),
	};

	default String toDisplayName(String instrumentName) {
		var displayName = instrumentName.substring(0, 1).toUpperCase() + instrumentName.substring(1);
		displayName = instrumentName.replace('_', ' ');	
		return displayName;
	}
}
