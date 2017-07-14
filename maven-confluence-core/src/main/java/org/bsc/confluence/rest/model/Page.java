/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bsc.confluence.rest.model;

import javax.json.JsonObject;
import org.bsc.confluence.ConfluenceService.Model;

/**
 *
 * @author bsorrentino
 */
public class Page implements Model.Page {

	public final JsonObject data;

	public Page(JsonObject delegate) {
		if (delegate == null) {
			throw new IllegalArgumentException("delegate argument is null!");
		}
		this.data = delegate;
	}

	@Override
	public String getId() {

		return data.getString("id");
	}

	@Override
	public String getTitle() {
		return data.getString("title");
	}

	@Override
	public String getSpace() {
		return data.getJsonObject("space").getString("key");
	}

	@Override
	public int getVersion() {
		return data.getJsonObject("version").getInt("number", 0);
	}

	@Override
	public String getParentId() {
		return String.valueOf(data.getJsonObject("container").getInt("id"));
	}

	@Override
	public String toString() {
		return data.toString();
	}

}
