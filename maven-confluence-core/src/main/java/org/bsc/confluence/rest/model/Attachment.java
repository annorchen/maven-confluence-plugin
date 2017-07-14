package org.bsc.confluence.rest.model;

import java.util.Date;
import javax.json.JsonObject;
import org.bsc.confluence.ConfluenceService.Model;

public class Attachment implements Model.Attachment {

	public final JsonObject data;

	public Attachment(JsonObject delegate) {
		if (delegate == null) {
			throw new IllegalArgumentException("delegate argument is null!");
		}
		this.data = delegate;
	}

	@Override
	public void setFileName(String name) {
		// TODO Auto-generated method stub
	}

	@Override
	public String getFileName() {
		if (data.getJsonArray("results") == null || data.getJsonArray("results").isEmpty()) {
			return null;
		}
		return data.getJsonArray("results").getJsonObject(0).getString("title");
	}

	@Override
	public void setComment(String comment) {
		// TODO Auto-generated method stub
	}

	@Override
	public Date getCreated() {
		if (data.getJsonArray("results") == null || data.getJsonArray("results").isEmpty()) {
			return null;
		} else {
			String[] split = data.getJsonArray("results").getJsonObject(0).getJsonObject("_links").getJsonString("download").getString().split("&");
			Long timestampe = 0L;
			for (int i = 0; i < split.length; i++) {
				if (split[i].startsWith("modificationDate=")) {
					timestampe = Long.parseLong(split[i].substring(split[i].indexOf("=") + 1));
					break;
				}
			}
			return new Date(timestampe);
		}
	}

	@Override
	public void setContentType(String contentType) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getId() {
		if (data.getJsonArray("results") == null || data.getJsonArray("results").isEmpty()) {
			return null;
		}
		return data.getJsonArray("results").getJsonObject(0).getString("id");
	}

}
