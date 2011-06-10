package de.saces.fnplugins.Shoeshop;

import freenet.l10n.BaseL10n;

public class L10nableError extends Exception {
	private static final long serialVersionUID = 1L;

	static BaseL10n _intl;

	public L10nableError(String string) {
		super(string);
	}

	static void setL10n(BaseL10n intl) {
		_intl = intl;
	}

	@Override
	public String getLocalizedMessage() {
		return _intl.getString(getMessage());
	}

}
