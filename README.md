# core-api

The Core API is an experiment in software architecture. Like other architecture
APIs, its noble goal is to improve software quality by allowing its users to
more succintly and more empirically describe their code's goals.

The idea for the Core API arose from a hypothesis I had: as software engineers,
we only ever really want our code to do one of two things.

1. Transform data from one form to another
2. Move data from one "place" (read: region of byte storage) to another, at a 
   specific point in time

(Note: I could be totally wrong about this, but I honestly can't think of 
something I normally would want my code to do that isn't (or can't be 
restructured as) one of these two things.)

My ultimate goal with the Core API is to test this hypothesis, by presenting an
interface that allows the user to directly address those two concerns and 
nothing more.

## How do we write programs then?

You can theoretically write entire specifications for programs along the lines
of:

When {some event} happens, grab the data from {some places}, transform it by 
{some transformation}, and put the transformed data into {some other places}.

Here are some examples:

- **A video-game**: When 1/60th of a second (or whatever your desired frame rate 
  is) has passed, grab the data from the input devices and the current game 
  state, transform it by the game loop, and put the transformed data into the 
  current game state and the screen.

- **A web server**: When a client makes a request, grab the data from that 
  request (and probably some other data from a database), transform it into an 
  HTTP response, and put the transformed data into a port to send to the 
  requesting client.

- **An automaton/robot**: When the environment triggers the robot's sensors, 
  grab the data from the sensors, transform it, and put the transformed data 
  into the output ports of the chip (so that the robot could do useful things 
  like move or produce sound or whatever).

And there are many more that quite neatly fit this description. These examples
are here to hopefully convey the breadth of programs that this model can 
produce.

The Core API has as direct a translation of this model as I could make in a
lisp (and hopefully more languages, as well).

## No really, how do we write programs with this?

The most important function in the Core API is `on-event`, which has the 
following signature:

    (on-event context event-name in-places xform)

- **context**: Not all hardware is built the same, and it's very likely that 
  the I/O ports on a robot will be very different from the I/O ports on a 
  personal computer. The "context" is `on-event`'s gateway to all the I/O 
  "places" that can be read from or written to. It should contain 3 maps:
    - **in-places**: a map of "places" the program can read; the keys to the map
      are the names of the places (usually keywords in Clojure)
    - **out-places**: a map of "places" the program can write to; the keys to
      the map are the names of the places (usually keywords in Clojure)
    - **events**: a map of "events" that could occur during the time a program
      is run; the keys to the map are the names of the events (usually keywords
      in Clojure)
- **event-name**: This is the name of the event to subscribe to. This should be 
  one of the event names in the `context`'s map of events. An event is similar 
  to the concept from event-driven OOP, except that it *only* exists to 
  represent *points in time*, so it cannot "carry" any data with it.
- **in-places**: This is a vector of the names of places to read from when the 
  event specified by `event-name` occurs. The names should all be names of
  places in the `context`'s map of in-places.
- **xform**: This is a function that gets called when the event specified by
  `event-name` occurs. It takes a map of `in-place` names to values and produces
  a map of `out-place` names to values. Each entry in the returned map will be
  "written" to the specified `out-place`.

Let's look at a basic use case for this function:

```clojure
(defn print-hello [_]
  {my-context/stdout "Hello!"})

(defn -main [& args]
  (on-event my-context/context :start [] print-hello))
```

I'd like to stray from the specifics of `my-context` for right now (it will be
covered later). For our purposes now, just consider that not all hardware will 
have a place called `stdout` to put data into.

Let's dissect this:
- `print-hello` returns a map with one entry: `stdout => "Hello!"`. This means
  that `on-event` will take the string `"Hello"` and put it into the `stdout`
  place when the specified event occurs. It takes `_` as its parameter to 
  specify that it needs to take a parameter (all "event handler" functions take
  one parameter), but it is not used.
- The call to `on-event` uses the following as its parameters:
    - **context**: as mentioned previously, this is a map with platform 
      information on it
    - **event-name**: `:start` is an arbitrary name for an event inside the 
      `context`; for this particular context, the intention is for it to name
      the event that gets fired when the program starts up
    - **in-places**: The empty vector for this argument implies that the 
      function to be called when the event named `:start` occurs *doesn't read 
      from any input places*. The particular function just "puts" the string
      `"Hello"` into `stdout` -- no reading required.
    - **xform**: This is where we put our `print-hello` function. This will be
      called when `:start` happens.

You can read this as:

When the event named `:start` happens, grab *nothing* from input places, 
transform that nothing by the function `print-hello`, and put the result into
`stdout`.

Let's look at a slightly more complex example:

```clojure
(defn echo-user-input [place-map]
  (let [user-input (:stdin place-map)]
    {my-context/stdout user-input}))

(defn -main [& args]
  (on-event my-context/context :read-line-event [my-context/stdin] echo-user-input))
```

There are a couple of things that are different between this example and the 
previous one:
- The `xform` function now actually uses (and names) its one parameter.
- The `xform` function "grabs" the data inside the place named `:stdin` and uses 
  it in the body of the function. Specifically, it puts that value into 
  `stdout`.
- The `in-places` parameter to `on-event` now has one element: `stdin`
- The `event-name` parameter to `on-event` is now `:read-line-event`. This is
  also an arbitrary event name. For the purposes of this example, we'll assume
  that it's an event that gets triggered when the user inputs something into
  stdin (i.e. a call to `read-line` returns).
- (Brief aside): Note that `context/stdin` in the `on-event` call is different
  from the `:stdin` tag used inside `echo-user-input`. There is a reason for 
  this that I will go over later.

You can read this as:

When the user inputs a line into stdin, grab the data from stdin, transform it
by the function `echo-user-input`, and put the result into `stdout`.

## The Place API

Ideally, a platform's "context" would just contain low-level "places". For 
simple, bare platforms, that could mean just straight-up I/O ports for the 
places.

While these would be nice for platform creators to provide, generally 
programmers don't want to deal directly with I/O ports. When I say "put the
string 'Hello' into stdout," that could mean a bunch of individual places get
written to. Conversely, when I say "grab the mouse position", that means
that I actually want to read data from more than one place.

The *Place API* exists to do just that. It contains just two functions, 
`connect` and `spread`.

`connect` takes multiple input-places and a function to grab their data and
produce a single value. It creates a new input-place whose value comes from the
function.

On the other side of the coin, `spread` takes a function that writes to multiple
output-places given a single value. It creates a new output-place.

Hopefully, this professional dataflow diagram will make it easier to visualize.
I paid someone $5 US to make this for me so it better work.

```
Input                                                       Output
in-place A                                             out-place F
          \                                           ^
           \                                         /
            v                                       /
in-place B---> in-place D --xform--> out-place E ----> out-place G
            ^                                       \
           /                                         \
          /                                           v
in-place C                                             out-place H

=============>                                      =============>
connecting                                               spreading
```

Here's the deets for `connect`:

    (connect place-name dependent-places connect-fn)

- **place-name**: The key in the map passed to `xform` in `on-event` 
  representing the new, generated place.
- **dependent-places**: Similar to the `in-places` parameter to `on-event`, this
  is a vector of the places to read from before calling `connect-fn`. These
  places can even be "connected" places themselves!
- **connect-fn**: This is a function that takes a map of the `dependent-place`
  names and values. It should produce a single value that will be keyed by 
  `place-name` when used in an event handler function passed to `on-event`.

Example:

```clojure
(def input-with-timestamp
  ;; this is a call to "connect"; the notion being that data that comes from
  ;; the place generated by this call is actually from multiple other places
  (place/connect
    :input-with-timestamp                   ;; the "name" of the new place
    [context/stdin context/current-time]    ;; the other places it gets its data from
    (fn [place-map]                         ;; a function that creates the value
                                            ;;  that the "reader" of the place sees
      (str (:current-time place-map) ": " (:stdin place-map)))))
```

In this example, we create a new place that contains a string with the most 
recent user-input string and a timestamp (i.e. `"10:42:23 UTC: hello"`). If this
place is used in an event handler, then inside the `place-map` you'll see an
entry that looks like `:input-with-timestamp => "10:42:23 UTC: hello"`.

And here's the lowdown on `spread`:

    (spread spread-fn)

- **spread-fn**: This is a function that takes one value and returns a map of 
  out-places to values. It'll look a lot like a event handler for `on-event`
  except instead of taking a `place-map` it'll just be a single value. The
  output places can even be "spread" places themselves!

Example:

```clojure
(def echo-and-send 
  ;; this a call to "spread"; the notion being that all data sent to this place
  ;; will actually be sent to multiple other places by a function
  (place/spread
    (fn [val]
      {context/stdout val
       context/ip-port 8000 val})))
```

## Some extra details

There might be something that felt slightly off to you when reading the 
descriptions of `connect` and `spread`. `connect` takes 3 whole arguments to do
what is essentially the opposite of what `spread` is doing with 1 argument. 
What's going on?!

Well I'm glad you asked, Action Jackson. It has to do with a quirk in the order
of operations. When an event triggers, here's what happens, in order:

1. All input places (specified by the `in-places` argument to `on-event`) are 
   read. Their values are stored in a map keyed by their place names.
   (i.e. `{:stdin "hot diggity dogg" :current-time "0:12:34 UTC"}`)
2. `xform` is called with the map generated in step 1 as its lone parameter.
   This generates a new map that looks similar to step 1's, but the keys are
   output places instead of names of input places.
   (i.e. `{my-context/stdout "HOT DIGGITY DOGGG" client-port "oh yeah"}`)
3. Output places are written to with their values assigned to the values from
   the map generated in step 2.

Notice how the keys in the output map *aren't* the names of the output places, 
but the keys to the input map *are* the names of the input places.

When a map of outputs to values are generated from an event handler, the output
looks like this in code:

```clojure
{my-context/stdout "HOT DIGGITY DOGGG"}
```

but `my-context/stdout` is actually this:

```clojure
{:place-tag :raw :place-name :stdout}
```
making the whole map look like:

```clojure
{{:place-tag :raw :place-name :stdout} "HOT DIGGITY DOGGG"}
```

Now, I know what you're thinking: "Whoa! What the heck does that mean? Why would
you even do that? You're dumb. I hate you. Go back to Canada."

Here's the deal, Jerry. That extra information is needed to differentiate
spread places from regular, platform-level places ("raw" places). So when you
write to a spread place, here's what its key in the step 2 map looks like:

```clojure
{:place-tag :spread :spread-fn (fn [val] (...))}
```

Because the input map is "pre-loaded" before being passed to `xform` using 
*keywords* as keys, and the output map uses the *actual place information* as 
keys, there is a dissymmetry between how input places and output places need to 
be created. 

`connect` needs to know how to "pre-fill" its entry in the map sent to 
`xform`. In particular, it needs to know the *key* that the connected value will
use in that map.

`spread` on the other hand, waits until `xform` is complete to have its actual,
base-level places evaluated, so all it needs to know is how to take its one
value and supply it to yet-unevaluated places.

## Still to do
If you got this far down actually reading everything, then congratulations. I am
fully aware that this README is probably not in a perfect state and that there
are likely parts that are confusing or overly complicated. Please feel free to
tell/ask me about anything in the repo and/or README. I would sincerely 
appreciate suggestions.

To Clojurists, I apologize if my coding style or misuse/underuse of Clojure 
features disgusts you. I am vastly inexperienced with the language. Please let
me know how I can improve.

This repo is primarily here as an idea. It is in no way production-ready, mainly
because I haven't filled out any real context to use. The events and places I
use in the repo were created out of necessity to build the example code. If I
feel confident that the API works and is useful, I will certainly consider 
building a "standard context".

Here are some other things I'd like to get done:
- standard means for the user to generate and apply their own context
- ways to easily combine or add functionality to event handlers
- WAYYYYYY more examples
- build a timeout scheduler
- port the API to another language, probably one ML-variant and 1/2 curly brace
  languages

## License

Copyright © 2016 Richard Gebbia

Distributed under the Eclipse Public License either version 1.0 or any later version.
