package org.kairosdb.client.deserializer;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.kairosdb.client.DataPointTypeRegistry;
import org.kairosdb.client.builder.DataPoint;
import org.kairosdb.client.response.GroupResult;
import org.kairosdb.client.response.Result;
import org.kairosdb.client.response.grouping.DefaultGroupResult;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static org.kairosdb.client.util.Preconditions.checkState;

public class ResultsDeserializer implements JsonDeserializer<Result>
{
	private DataPointTypeRegistry typeRegistry;

	public ResultsDeserializer(DataPointTypeRegistry typeRegistry)
	{
		this.typeRegistry = requireNonNull(typeRegistry);
	}

	@Override
	public Result deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
	{
		// Name
		String name = json.getAsJsonObject().get("name").getAsString();
		checkState(name != null, "Missing name");

		// Tags
		JsonElement tagsElement = json.getAsJsonObject().get("tags");
		Map<String, List<String>> tags = context.deserialize(tagsElement, new TypeToken<Map<String, List<String>>>()
		{
		}.getType());

		// Group_By
		JsonElement group_by = json.getAsJsonObject().get("group_by");
		List<GroupResult> groupResults = context.deserialize(group_by, new TypeToken<List<GroupResult>>()
		{
		}.getType());

		List<DataPoint> dataPoints = new ArrayList<DataPoint>();
		if (group_by != null)
		{
			String type = null;
			for (GroupResult groupResult : groupResults)
			{
				if (groupResult.getName().equals("type"))
				{
					type = ((DefaultGroupResult) groupResult).getType();
				}
			}
			checkState(type != null, "Missing type");

			// Data points
			final Class dataPointValueClass = typeRegistry.getDataPointValueClass(type);
			checkState(dataPointValueClass != null, "type: " + type + " is not registered to a custom data type.");

			JsonArray array = (JsonArray) json.getAsJsonObject().get("values");
			for (JsonElement element : array)
			{
				JsonArray pair = element.getAsJsonArray();
				dataPoints.add(new DataPoint(pair.get(0).getAsLong(), context.<DataPoint>deserialize(pair.get(1), dataPointValueClass)));
			}
		}

		return new Result(name, tags, dataPoints, groupResults);
	}
}
