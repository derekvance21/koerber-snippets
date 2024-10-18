Tip: The suggestions widget supports CamelCase filtering, meaning you can type the letters which are upper cased in a method name to limit the suggestions. For example, "cra" will quickly bring up "createApplication".

so I've noticed this. For completion items, if you type crapplic, it'll match "**cr**eate**Applic**ation". This could be used to not have to do all the permutations.

Ala, type ormstopkd, and (in theory, this could match a snippet named `ormPkdSto`...

turns out, this doesn't actually work. The camelcase matches still need to be in order.
So typing stopkditm would still be able to match stoPkdHumItm

- [X] `ord` also has a foreign key on `order_id` to `orm`. And `orm`'s primary key clustered is `order_id`, so it might be better to make the edge from `ord` to `orm` to be `order_id` rather than (`wh_id`, `order_number`).
