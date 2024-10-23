# Koerber Snippets

This is a project to generate useful SQL snippets for Koerber development, specifically for AAD database.

## Background

Here is a graph of a subset of the AAD schema. Each node is a table, and each relationship between two tables is an edge. Each node has a unique default alias. Each edge has a join attribute describing how one table joins to the other table's primary key.

![AAD Schema as a graph](schema.png)

## Snippets

SSMS and Azure Data Studio (also VSCode) support custom snippets.

### FROM Snippet

Each table in the schema can expand a table alias to a `FROM` line with `WITH (NOLOCK)`. So `sto` expands to:
```sql
FROM t_stored_item sto WITH (NOLOCK)
```

### JOIN Snippet

Here is the cool part. Type up to three table aliases, and a snippet is created that is *a* shortest set of paths from the first alias to the other two aliases. For example, `stohumloc` would expand to:
```sql
JOIN t_hu_master hum WITH (NOLOCK)
	ON sto.wh_id = hum.wh_id
	AND sto.hu_id = hum.hu_id
JOIN t_location loc WITH (NOLOCK)
	ON sto.wh_id = loc.wh_id
	AND sto.location_id = loc.location_id
```
Because these two edges, from `sto` to `hum` and from `sto` to `loc` is the shortest set of paths starting at `sto` that visits both `hum` and `loc`.

Typing `zonsto` would expand to:
```sql
JOIN t_zone_loca znl WITH (NOLOCK)
	ON zon.wh_id = znl.wh_id
	AND zon.zone = znl.zone
JOIN t_location loc WITH (NOLOCK)
	ON znl.wh_id = loc.wh_id
	AND znl.location_id = loc.location_id
JOIN t_stored_item sto WITH (NOLOCK)
	ON loc.wh_id = sto.wh_id
	AND loc.location_id = sto.location_id
```
Because the shortest path from `zon` to `sto` is through `znl`, and `loc`.

You have to be careful, though, because there are multiple shortest paths from a source to a set of destinations. For example, say you wanted to find all the `sto` on each employee's fork. So you could try `stoemp`, but you'd get:
```sql
JOIN t_pick_detail pkd WITH (NOLOCK)
	ON sto.type = pkd.pick_id
JOIN t_employee emp WITH (NOLOCK)
	ON pkd.user_assigned = emp.id
```
Joining through pkd is probably not what you want. So you can "refine" the snippet by specifying another table you want involved. So a fork is a location (`loc`), so you can insert `loc` in the middle or end of the snippet, and now `stolocemp` gives you:
```sql
JOIN t_location loc WITH (NOLOCK)
	ON sto.wh_id = loc.wh_id
	AND sto.location_id = loc.location_id
JOIN t_employee emp WITH (NOLOCK)
	ON loc.c1 = emp.id
```
