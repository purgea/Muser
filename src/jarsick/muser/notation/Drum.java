/******************************************************************************
 * Copyright (c) 2022, Daniele Aurigemma
 * All rights reserved.
 * 
 * Part of the Muser project github: https://github.com/Jarsick/Muser
 */

package jarsick.muser.notation;

public enum Drum{
	KICK(36),
	SNARE(28),
	HAT(42),
	CRASH(49),
	RIDE(51),
	LOW_FLOOR_TOM(41),
	HIGH_FLOOR_TOM(43),
	LOW_TOM(45),
	LOW_MID_TOM(47),
	HIGH_MID_TOM(48),
	HIGH_TOM(50),
	TAMBOURINE(54),
	COWBELL(56),
	VIBRASLAP(58),
	MUTE_TRIANGLE(80),
	OPEN_TRIANGLE(81),
	SILENCE(0);


	private int midiValue;

	Drum(int midiValue) {
		this.midiValue = midiValue;
	}

	public int getMIDI(){
		return midiValue;
	}
}
