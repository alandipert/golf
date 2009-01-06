Getting Started With Golf
=========================

1. Compile Golf.jar:
        
        $ cd <Golf directory>
        $ ant

2. Start the server:

        $ cd <Golf directory>
        $ ./Golf-start.sh -p <port> <appname>@<path/to/approot> [<appname2>@<path/to/approot2> ...] 

3. Access the application: point your browser to <http://hostname:port/appname/>.

There are some example apps in the examples/ directory to see how
everything works and enjoy. Especially interesting is the behavior of
these DHTML applications in a browser with javascript disabled. Try it
in lynx and see the app the way the googlebot does!

Golf applications form the user interface for web services APIs. A
Golf application is not complete, as such. In order to be useful, Golf
applications must interact with a separate backend service, usually via
a RESTful API.

Introduction
------------

Golf applications form the user interface for web services APIs. A
Golf application is not complete, as such. In order to be useful, Golf
applications must interact with a separate backend service, usually via a
RESTful API. Golf applications are responsive, fully dynamic "2.0" style
interfaces. They are also, however, real HTML documents, as well---fully
accessible to non-javascript browsers and search engine spiders.

Golf applications can be developed rapidly and naturally due to the
extreme simplicity of the architecture. A proper separation of content
and presentation is required, which encourages good application design
from the beginning. Finally, the Golf component architecture facilitates
easy reusability of your view elements.

Golf applications must be served (at least partly---we'll see why later)
by the Golf application server. The Golf application server provides
three primary services:

1. __clientside MVC framework:__ a javascript MVC framework with
   template-based views and ActiveRecord style models, and a javascript
   controller with actions implementing callbacks corresponding to an action
   lifecycle (allowing the controller to save and restore its state when
   invoked or destroyed)
2. __zero-configuration Cloudfront caching:__ an optional, fully automatic
   and transparent AWS Cloudfront caching of 99% of the Golf application
   (clients use JSONP to fetch UI elements from Cloudfront---we'll see
   how later)
3. a serverside javascript proxy which enables fully dynamic AJAX DHTML
   Golf apps to work reliably and transparently in non-javascript browsers
   (googlebot, for example), with zero redundant code or special effort
   required (easier to show than tell, so check out the demo!)

Components
----------

In Golf, screens are constructed of components. Components are similar
to what would be called "partials" in Rails---independent HTML fragments
that can be inserted into the page during construction. You can think
of Golf components as the elementary particles that make up the user
interface. No content smaller than a full component can be added to
a screen (although there are certain exceptions to this rule), and no
content smaller than a full component can be removed. All of the HTML
elements in a component are under the exclusive control of that component.
No other component can access them. Communication between components
occurs through a system of custom events, forming a tight internal API
and facilitating a modular, reusable structure.

Components consist of three parts: an HTML template, a javascript
transformation, and a CSS file. Each of these files is written as though
it were the entire document. This is possible because Golf carefully
sandboxes the HTML, javascript, and CSS, and restricts any effects and
access to the component itself. For example, doing

    $(".myclass")

in your javascript transformation will only return elements from within
the component, and not from any other, or even another instance of
this component.

###Example Component

Let's take a quick look at a simple component, just to solidify the
concepts here.

_hello.html:_

    <div>
        <h1 class="big_title">Here's some title text!</h1>
        <div class="hide_show">Button</div>
    </div>

_hello.js:_

    $(".hide_show").click(function() {
        $(".big_title").toggle();
        $(".hide_show").text($(".hide_show").text() == "hide" ? "show" : "hide");
    });
    
    $(".big_title").text("Hello, " + $.argv.username + "!");
    $(".hide_show").text("hide");

_hello.css:_

    h1 {
        color: #FCFCFC;
    }
    
    .hide_show {
        background-color: orange;
    }

This component would be instantiated in the application by doing something
like this (assuming that the files are located in the 
_components/com/thinkminimo/_ directory relative to the approot):

    new Component("com.thinkminimo.hello", base, { username: "bob" });

What happens when the component is instantiated is this: First, the HTML
and javascript files are fetched using AJAX. Then the HTML template is
inserted into the DOM and a &lt;link&gt; tag is created in the document
head to load the CSS for the component. Then the javascript transformation
is run, replacing the dummy content with real content and setting up
the dynamic behaviors. Don't worry if this is vague or unclear to you
at this point; it'll become natural as we go along. The main point to
understand here is the structure of the component, and the relationship
between the three parts, the HTML template, the javascript transformation,
and the CSS.

Note that this little fragment of HTML, javascript, and CSS is completely
atomic. Because of the magical sandboxing of the Golf runtime, it can be
inserted anywhere in the document as a little independent, self-contained
widget, complete with its own internal dynamic behaviors, styles, and
interfaces. Just instantiate it, insert it into the DOM, and let it
go. Fire and forget, basically. That's the goal of components.

Controllers
-----------

The controller is the entry point of the application. That is to say,
when a URL is requested by the client, that request is delegated to one
of the controller's _actions_ for servicing. The controller forms the
bridge between the models and the views, i.e. hooking the content and
business logic in the backend application interface to the components
in the frontend user interface.

Later on we'll see a number of default controller behaviors that are
included in the Golf runtime to make your job a lot easier.

###Example Controller

Again, the best way to illustrate the concept is probably just to present
a simple example controller, and hopefully make the idea more concrete.

_controller.js:_

    jQuery.golf.actions = {

        home: function(base, argv) {
            base.empty();
            new Component("com.thinkminimo.hello", base, { username: argv[1] });
        },

        contact: function(base, argv) {
            base.empty();
            new Component("com.thinkminomo.contact", base);
        },

    };

Here we have two controllers defined: _home_ and _contact_. Incidentally,
the _home_ controller is the default. The _base_ argument is the
jQuery-wrapped document body, and the _argv_ argument is the list of
path elements passed in the URI.

> Note: URIs will be parsed by the Golf runtime. Golf expects a URL of
> the form _http://host.com:port/app/action/arg1/arg2/.../argN/_. This
> request will be delegated to the _action_ with the argv argument set
> to _\[arg1, arg2, ..., argN\]_.

Glosssary
=========

Here's a rundown of the Golf jargon you might have seen:

__approot:__ The base directory of your Golf application.
