package org.usask.srlab.coster.core.dictionary;

import org.usask.srlab.coster.core.dictionary.APIDictionary;

public abstract class APIElement {
	
	private int nameId;
	
	public APIElement(int id) {
		this.nameId = id;
	}
	
	int getNameId() {
		return nameId;
	}

	String getName() {
		return APIDictionary.getName(nameId);
	}
	
	abstract public String getFQN();
	
}
