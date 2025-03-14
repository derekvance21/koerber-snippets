Tip: The suggestions widget supports CamelCase filtering, meaning you can type the letters which are upper cased in a method name to limit the suggestions. For example, "cra" will quickly bring up "createApplication".

so I've noticed this. For completion items, if you type crapplic, it'll match "**cr**eate**Applic**ation". This could be used to not have to do all the permutations.

Ala, type ormstopkd, and (in theory, this could match a snippet named `ormPkdSto`...

turns out, this doesn't actually work. The camelcase matches still need to be in order.
So typing stopkditm would still be able to match stoPkdHumItm

- [X] `ord` also has a foreign key on `order_id` to `orm`. And `orm`'s primary key clustered is `order_id`, so it might be better to make the edge from `ord` to `orm` to be `order_id` rather than (`wh_id`, `order_number`).

## Brainstorming

directed graph!!!
that should really cut down on generated path lengths, I think.
I think it'll be rare to have strings of joins more than 3 tables.

so a table can have *one* primary key and multiple unique indexes.
so how am I supposed to give a key to each unique index? I suppose that's why they name indexes
ex: t_carrier has a primary key (carrier_id) and two unique indexes (scac_code and carrier_code)
also t_stored_item has sto_id as primary key clustered, as well as unique nonclustered :'|

does ormpkdhumord make sense? does it matter?
it's almost like there will be directed edges, or something
like you can go ormord, but once you commit to going one to many, you can only do one to one's from there, or something
you can do as many one-to-one's as you like, but you can only have one one-to-many!

so maybe you can have as many out-edges as you want, but only one in-edge?
orm has outs to car, ldm, cli
orm has in from ord, hum, pkd, etc.

here's a problematic example. does pkd join to ord?
order to order? neither is unique/primary key
order/line to order/line? niether is unique/primary
maybe you make a rule that its only joins that involve primary/unique keys? below system would help enforce that during data entry
does hum join to pkd? Both have order_number information...
but you'd never really do that join!!!
like why would you want to know - ok here's a hum record. Now give me all the pkd records with that are
assigned to the same order as this hum. Like doesn't really make sense.
I have to constrain the size of this graph, probably, or there's going to be *tons* of snippets
This rule will be fine. Not a big deal

if you can join hum to orm, then you can also join hum directly to ord and pkd and anything else that uses orm's PK
for some things that's not the case... like you can't go sto to orm. but you can go sto to pkd to orm
so it's like: edge means that src table can hit destination's PK.
sto can hit loc, hum, pkd, etc.'s PK.
so the table nodes should have their primary key as part of their attributes
and then the edge is [src dest {:fk [:k ...]}]
ex: [:sto :pkd {:fd [:type]}] - sto has an edge to pkd through its type field. 
that might limit me, but I want to roll with that for a bit
this makes the graph build ordering important.
[:ord :orm {:fk [:wh_id :order_number]}] is valid, where
[:orm :ord {:fk [:wh_id :order_number]}] is not - ord's PK is not wh_id, order_number
IMPORTANT - it doesn't have to be just PK, though, it could be UNIQUE INDEX!
ex: ord's PK is order_detail_id (but who uses that??), whereas its UNIQUE INDEX is (wh_id, order_number, line_number)

stolocitmalo - what does this mean?
Here's what it means - I'm joining sto to alo, but I want them to join through *both* itm and loc.
So:
```sql
JOIN t_allocation alo WITH (NOLOCK)
    ON sto.wh_id = alo.wh_id
    AND sto.location_id = alo.pick_location
    AND sto.item_number = alo.item_number
```
That is kind of useful, not incredibly so... You'd end up creating a **ton** of joins that you'll almost never use

`sudo apt install graphviz`

# Steiner Trees

> One well-known variant, which is often used synonymously with the term Steiner tree problem, is the Steiner tree problem in graphs. Given an undirected graph with non-negative edge weights and a subset of vertices, usually referred to as terminals, the Steiner tree problem in graphs requires a tree of minimum weight that contains all terminals (but may include additional vertices) and minimizes the total weight of its edges.
- <a href="https://en.wikipedia.org/wiki/Steiner_tree_problem"><cite>Steiner tree problem</cite></a>

[Solutions to Steiner Tree](https://www.cs.ucr.edu/~michalis/COURSES/240-08/steiner.html)
