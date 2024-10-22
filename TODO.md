- [X] for each node, just have one possible path. So pkd would have pkdsto, pkdhum, pkdorm, etc.
    And if you wanted sto -> hum -> orm instead of sto -> pkd -> orm, for example.
    You'd have to do stohum, then humorm, instead of just stoorm, which would do the latter.
- [ ] TODO - probably should be snippet for generating the FROM, as well as the JOINs, in one snippet
*stopkd ->
FROM t_stored_item sto WITH (NOLOCK)
JOIN t_pick_detail pkd WITH (NOLOCK)
    ON sto.type = pkd.pick_id
- [ ] Make it a undirected graph and see what happens! :O
- [ ] multiple nodes per snippet
    - here was the motivating thing - breaking ties. I was thinking a common thing is finding sto on people's fork. So naturally I tried `stoemp`. But there were multiple shortest paths, and it happened to do `sto` to `pkd` to `emp` first. And that's not what I wanted - I wanted `sto` to `loc` to `emp`. So you don't necessarily have to input the entire path, but you still get snippet uniqueness.
    - `stolocemp` is: what is the shortest path from `sto` to `emp` through `loc`
    - And some way I think you can unify this with `stohumloc`, for example, to get multiple paths with a given start.

