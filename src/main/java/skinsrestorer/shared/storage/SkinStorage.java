/**
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package skinsrestorer.shared.storage;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.reflect.TypeToken;
import skinsrestorer.shared.format.Profile;
import skinsrestorer.shared.format.SkinProfile;
import skinsrestorer.shared.utils.IOUils;

public class SkinStorage {

	private static final SkinStorage instance = new SkinStorage();

	public static SkinStorage getInstance() {
		return instance;
	}

	private static final String cachefile = "cache.json";
	private static final Gson gson = new GsonBuilder().registerTypeHierarchyAdapter(SkinProfile.class, new SkinProfile.GsonTypeAdapter()).setPrettyPrinting().create();
	private static final Type type = new TypeToken<Map<String, SkinProfile>>(){}.getType();

	protected static File pluginfolder;

	public static void init(File pluginfolder) {
		SkinStorage.pluginfolder = pluginfolder;
		instance.loadData();
	}

	private final ConcurrentHashMap<String, SkinProfile> skins = new ConcurrentHashMap<String, SkinProfile>();

	public boolean isSkinDataForced(String name) {
		SkinProfile profile = skins.get(name.toLowerCase());
		if (profile != null && profile.isForced()) {
			return true;
		}
		return false;
	}

	public void removeSkinData(String name) {
		skins.remove(name.toLowerCase());
	}

	public void setSkinData(String name, SkinProfile profile) {
		skins.put(name.toLowerCase(), profile.cloneAsForced());
	}

	public SkinProfile getOrCreateSkinData(String name) {
		return skins.compute(name.toLowerCase(), (playername, profile) -> profile != null ? profile : new SkinProfile(new Profile(null, playername), null, 0, false));
	}


	public void loadData() {
		try (InputStreamReader reader = IOUils.createReader(new File(pluginfolder, cachefile))) {
			Map<String, SkinProfile> gsondata = gson.fromJson(reader, type);
			if (gsondata != null) {
				skins.putAll(gsondata);
			}
		} catch (JsonIOException | IOException e) {
		}
	}

	public void saveData() {
		pluginfolder.mkdirs();
		try (OutputStreamWriter writer = IOUils.createWriter(new File(pluginfolder, cachefile))) {
			gson.toJson(
				skins.entrySet()
				.stream()
				.filter(entry -> entry.getValue().shouldSerialize())
				.collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue())),
				type, writer
			);
		} catch (JsonIOException | IOException e) {
			e.printStackTrace();
		}
	}

}
