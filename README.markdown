Getting Started With Golf
=========================

1. Compile golf.jar:
        
        $ cd <golf directory>
        $ ant

2. Start the server:

        $ cd <golf directory>
        $ ./golf-start.sh -p <port> <appname>@<path/to/approot> [<appname2>@<path/to/approot2> ...] 

3. Access the application: point your browser to <http://hostname:port/appname/>.

There are some example apps in the examples/ directory to see how everything works and enjoy.

Architecture
------------

Golf applications are form the user interface for web services APIs. A golf application is not complete, as such. In
order to be useful, golf applications must interact with a separate backend service, usually via a RESTful API.

The golf application architecture is modular, with the following demarcations, proceeding from most to least general:

* Application
* Screen
* Component
* Element

The Application and Screen layers are particular to the application, and the Component and Element layers are general
and reusable across applications. We'll get into this a bit more deeply later, but first it's necessary to describe
golf's component structure in greater detail.

Components
----------

In golf, screens are constructed of components. Components are similar to what would be called "partials" in
Rails---independent HTML fragments that can be inserted into the page during construction. You can think of golf
components as the elementary particles that make up the user interface.

Components consist of three parts: an HTML template, a javascript transformation, and a CSS file. 