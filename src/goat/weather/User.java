package goat.weather;

/**
 * A simple javabean to represent a user
 */
public class User {

	String name;
	String location;

	public User() {
		name = "";
		location = "";
	}

	public User(String name, String location) {
		this.name = name;
		this.location = location;
	}

	public String getName() {
		return name;
	}

	public void setName(String userName) {
		this.name = userName;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}
}
