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

Golf applications are written in an MVC framework provided by the golf javascript runtime environment. Models and
controllers are defined in javascript, and components form the views in which content is presented to the user and in
which the user interacts with the application.

The golf application architecture is modular, with the following demarcations (proceeding from most to least general):

* Application (Controller)
* Screen (Model)
* Component (View)

The Application and Screen layers are particular to the application, and the Component and Element layers are general
and reusable across applications. We'll get into this a bit more deeply later, but first it's necessary to describe
golf's component structure in greater detail.

Components
----------

In golf, screens are constructed of components. Components are similar to what would be called "partials" in
Rails---independent HTML fragments that can be inserted into the page during construction. You can think of golf
components as the elementary particles that make up the user interface. No content smaller than a full component can 
be added to a screen (although there are certain exceptions to this rule), and no content smaller than a full
component can be removed. All of the HTML elements in a component are under the exclusive control of that component.
No other component can access them. Communication between components occurs through a system of custom events,
forming a tight internal API and facilitating a modular, reusable structure.

Components consist of three parts: an HTML template, a javascript transformation, and a CSS file. Each of these files
is written as though it were the entire document. This is possible because golf carefully sandboxes the HTML, javascript, and CSS, and restricts any effects and access to the component itself. For example, doing

    $(".myclass")

in your javascript transformation will only return elements from within the component, and not from any other, or even
another instance of this component.

###Example Component

Let's take a quick look at a simple component, just to solidify the concepts here.

_hello.html_

    1 <div>
    2     <h1 class="big_title">Here's some title text!</h1>
    3     <div class="hide_show">Button</div>
    4 </div>

_hello.js_

    1 $(".hide_show").click(function() {
    2     $(".big_title").toggle();
    3     $(".hide_show").text($(".hide_show").text() == "hide" ? "show" : "hide");
    4 });
    5
    6 $(".big_title").text("Hello World");
    7 $(".hide_show").text("hide");

_hello.css_

    1 h1 {
    2     color: #FCFCFC;
    3 }
    4
    5 .hide_show {
    6     background-color: orange;
    7 }