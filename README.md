# core-api

The Core API is an experiment in software architecture. Like other architecture
APIs, its noble goal is to improve software quality by allowing its users to
more succintly and more empirically describe their code's goals.

The idea for the Core API arose from a realization that, as software engineers,
we only ever really want our code to do one of two things:

1. Transform data from one form to another
2. Move data from one "place" (read: region of byte storage) to another, at a 
   specific point in time

(Note: I could be totally wrong about this, but I honestly can't think of 
something I normally would want my code to do that isn't (or can't be 
restructured as) one of these two things.)

You can theoretically write entire specifications for programs along the lines
of:

When {some event} happens, grab the data from {some places}, transform it by 
{some transformation}, and put the transformed data into {some other places}.

Here are some examples:

- A video-game: When 1/60th of a second has passed, grab the data from the 
  input devices and the current game state, transform it by the game loop, and 
  put the transformed data into the current game state and the screen.

- A web server: When a client makes a request, grab the data from that request
  (and probably some other data from a database), transform it into an HTTP
  response, and put the transformed data into a port to send to the requesting
  client.

- An automaton/robot: When the environment triggers the robot's sensors, grab
  the data from the sensors, transform it, and put the transformed data into the
  output ports of the chip (so that the robot could do useful things like move
  or produce sound or whatever).

## Still to do
- documentation behind the API proper
- documentation for the "place" API
- standard means for the user to generate and apply their own context
- ways to easily combine or add functionality to event handlers
- WAYYYYYY more examples

## License

Copyright © 2016 Richard Gebbia

Distributed under the Eclipse Public License either version 1.0 or any later version.
