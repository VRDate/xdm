package xdman.ui.components;

class PasswordItem {
	String host;
	String user;
	String password;

	@Override
	public String toString() {
		return host + "[" + user + "]";
	}
}
