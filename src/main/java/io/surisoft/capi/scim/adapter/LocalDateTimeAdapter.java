package io.surisoft.capi.scim.adapter;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LocalDateTimeAdapter extends XmlAdapter<String, LocalDateTime> {
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

	@Override
	public LocalDateTime unmarshal(String v) {
		if (v == null) {
			return null;
		}
		return LocalDateTime.parse(v, FORMATTER);
	}

	@Override
	public String marshal(LocalDateTime v) {
		if (v == null) {
	  		return null;
		}
		return FORMATTER.format(v);
	}
}