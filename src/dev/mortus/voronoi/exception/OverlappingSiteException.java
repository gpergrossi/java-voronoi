package dev.mortus.voronoi.exception;

import dev.mortus.voronoi.Site;

public class OverlappingSiteException extends RuntimeException {

	private static final long serialVersionUID = -5521256507536428244L;

	private String message;
	Site a, b;
	
	public OverlappingSiteException(Site a, Site b) {
		super();
		this.a = a;
		this.b = b;
		
		StringBuilder sb = new StringBuilder();
		sb.append("Duplicate position on sites "+a+" and "+b);
		this.message = sb.toString();
	}

	@Override
	public String getMessage() {
		return message;
	}
	
}
