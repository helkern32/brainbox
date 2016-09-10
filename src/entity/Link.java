package entity;

public class Link {
	private String href;
	private String name;
	
	public Link(String href, String name) {
		super();
		this.href = href;
		this.name = name;
	}
	
	public String getHref() {
		return href;
	}
	public void setHref(String href) {
		this.href = href;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
}
