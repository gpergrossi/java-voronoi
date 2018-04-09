# Java Voronoi

This Java code creates 2D Voronoi diagrams including the traversable output graph of site polygons, edges, and vertices. Can be used for delauney triangulation as well.

## In Action

Here is a gif created by the project's test.gpergrossi.tools.VoronoiAnimationGenerator class using the input 
```
svg f arf arf arf arf arf arf arf arf arf arf arf arf arf arf arf arf arf arf arf arf arf arf arf arf arf arf arf arf arf arf arf arf arf arf arf arf arf arf arf arf arf arf arf arf arf arf arf arf
```
The letters stand for:<br/>
**S**mall<br/>
**V**erbose (level 1)<br/>
**G**rid used to generate initial points<br/>
**A**dd point<br/>
**R**elax<br/>
**F**rame rendered to gif file<br/>

![Animated Voronoi relaxation with points added per frame](./animation.gif)

## Usage

1. Download the source code
2. Compile and run one of the following classes from test.gpergrossi.tools:
   - InfiniteVoronoi
      - scroll and pan with scroll wheel/middle click
   - VoronoiAnimationGenerator (seen above)
      - instructions shown when running
   - VoronoiTestView 
      - left click in the pentagon to place points
      - right click to delete
      - space to step through the algorithm
      - enter to finish the diagram
3. The test cases in test.gpergrossi.util use JUnit 5 (other versions will probably be fine too).

## Code Quality Note

I am using this code for my personal projects. I wrote all of it, but I did not clean it up for others to use.

## Pull Requests

I will, of course, look at all pull requests and potentially include them. I would love to work toward making this code a reliable and useful tool to as many programmers as possible.

## License

I am using the MIT license for this code. You may use the code as-is or modified in any project subject to the LICENSE.txt.
You must include a copy of this license in the documentation of any project that uses a subtantial portion of my code.
