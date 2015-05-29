![lein dependency](https://clojars.org/org.flatland/ordered/latest-version.svg)

ordered provides sets and maps that maintain the insertion order of their contents.

## Sets

    (use 'flatland.ordered.set)

    (ordered-set 4 3 1 8 2)
    => #ordered/set (4 3 1 8 2)

    (conj (ordered-set 9 10) 1 2 3)
    => #ordered/set (9 10 1 2 3)

    (into (ordered-set) [7 6 1 5 6])
    => #ordered/set (7 6 1 5)

    (disj (ordered-set 8 1 7 2 6) 7)
    => #ordered/set (8 1 2 6)

## Maps

    (use 'flatland.ordered.map)

    (ordered-map :b 2 :a 1 :d 4)
    => #ordered/map ([:b 2] [:a 1] [:d 4])

    (assoc (ordered-map :b 2 :a 1 :d 4) :c 3)
    => #ordered/map ([:b 2] [:a 1] [:d 4] [:c 3])

    (into (ordered-map) [[:c 3] [:a 1] [:d 4]])
    => #ordered/map ([:c 3] [:a 1] [:d 4])

    (dissoc (ordered-map :c 3 :a 1 :d 4) :a)
    => #ordered/map ([:c 3] [:d 4])


## Repositories

These two repositories are usually identical:

* https://github.com/amalloy/ordered
* https://github.com/flatland/ordered

The first is the canonical one.  Fixes tend to appear there first, and
that is the preferred repository for filing issues on Github.  Fixes
are backported to the second one periodically.
