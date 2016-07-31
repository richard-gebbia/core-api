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

## Still to do
- documentation behind the API proper
- documentation for the "place" API
- standard means for the user to generate and apply their own context
- ways to easily combine or add functionality to event handlers
- WAYYYYYY more examples

## License

Copyright © 2016 Richard Gebbia

Distributed under the Eclipse Public License either version 1.0 or any later version.
