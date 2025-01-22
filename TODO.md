- [ ] SEMI JOIN snippets - expands into EXISTS subquery. That's cool!
- [ ] `t_whse` and `t_whse_control` - maybe not as join snippets, but as node snippets
- [ ] think about `lgmpob`. That's going to be a `lgm.call_stack LIKE '%' + pob.name + '%'` type of join. But you don't even really want to join like that - `pob` table doesn't give you anything. This is where the where snippets would come in. Then you could do `wlgmpob` to get the above query, with `$1` instead of `pob.name`. And this is also something that you might want the derived edges for. And that would be a way to include that feature.
- [X] remove outbound edges from lkp
- [ ] a lot of tables have fields in weird orders. So usually you're manually writing out the fields you want to select. And that's annoying. I'm thinking about `trl` probably the most. So maybe an `strl` snippet to select the important fields off of `trl`, like `source/destination_location/hu_id`, for starters.
- [X] for each node, just have one possible path. So pkd would have pkdsto, pkdhum, pkdorm, etc.
    And if you wanted sto -> hum -> orm instead of sto -> pkd -> orm, for example.
    You'd have to do stohum, then humorm, instead of just stoorm, which would do the latter.
- [ ] TODO - probably should be snippet for generating the FROM, as well as the JOINs, in one snippet
*stopkd ->
FROM t_stored_item sto WITH (NOLOCK)
JOIN t_pick_detail pkd WITH (NOLOCK)
    ON sto.type = pkd.pick_id
- [X] Make it a undirected graph and see what happens! :O
- [X] multiple nodes per snippet
    - here was the motivating thing - breaking ties. I was thinking a common thing is finding sto on people's fork. So naturally I tried `stoemp`. But there were multiple shortest paths, and it happened to do `sto` to `pkd` to `emp` first. And that's not what I wanted - I wanted `sto` to `loc` to `emp`. So you don't necessarily have to input the entire path, but you still get snippet uniqueness.
    - `stolocemp` is: what is the shortest path from `sto` to `emp` through `loc`
    - And some way I think you can unify this with `stohumloc`, for example, to get multiple paths with a given start.
- [X] INNER JOINs ARE COMMUTATIVE!!
    - `stopkd` = `pkdsto`
    - should be an undirected graph when calculating joins! Agh!
- [ ] Certain nodes, while in shortest-path calculation, can only be start or end nodes
    - like if you go to lkp, you're done. You can't come out of there are again.
    - b/c a to lkp and lkp to b will never both join!
    - I think lkp is probably the exception in this b/c of the constant strings involved in the join
- [ ] "derived" edges
    - if a node has two in-edges that share the same join target on the node, then you can create a derived edge (just for shortest-path calculation) that skips the given node.
    - for example, loc has in-edges from alo and sto. Both have the same  join map's values (:wh_id and :location_id). So you can create a new edge from alo to sto with `{:wh_id :wh_id :putaway_location :location_id}`. But it's undirected. And it would also be an issue to break ties, there. Because this same concept applies with pkd, so there could be a join from alo to sto that goes through (skips) pkd. So with my current method it wouldn't quite necessarily work.
    - derived edges should have a higher weight. So `pkdwqa` would use the derived edge, but `pkdwkqwqa` would not use the derived edge, b/c that'd have a higher path cost than going pkd to wkq, wkq to wqa.
