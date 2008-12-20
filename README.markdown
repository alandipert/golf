
Introduction
============

###Golf Application Server

Golf is a web application server. Golf applications form the user interface 
for web services. 

###Data Layer

Since Golf applications are meant to be simply a user interface for a web
services API, Golf has no data store, no serverside component. Golf 
applications must interact with web services APIs. This effectively separates
your presentation from your content and business logic. Using AJAX to make
calls to a RESTful API from the Golf application is facilitated by the Golf
data layer javascript API, which forms a simple, zero-configuration,
object-oriented interface around your backend web service.

###Component Architecture

Golf applications are built with components, which
consist of an HTML template and a corresponding javascript transform. The
template is pure HTML and CSS, and the transform is pure javascript. There
is no serverside component. The javascript replaces dummy content in the
template with actual content, and manages dynamic behaviors between the
elements in the component. The javascript for each component is 
effectively restricted to operating on elements within itself. No XPath
selectors are necessary to get stuff done here, because each component
naturally knows all about its own internal topology.

###Javascript Proxying

Normally, all this templating and transforming will be done in the browser.
Some clients (like the googlebot web crawler, for example) do not
support javascript and dynamic HTML, however. In this relatively rare, but
absolutely crucial case, Golf will use the "headless" browser built into the
Golf application server to render the HTML. This is done transparently (from
the point of view of the application developer and the client). Dynamic
behaviors are properly accessible to javascript-less clients by way of this
proxying, with no need for extra code or redundant implementations.

###Cloud Computing and Content Delivery Networks

Since a Golf application is made up of a number of components, and since these
components are loaded asynchronously via AJAX calls, it is possible to cache
this content on a CDN like Amazon's Cloudfront, automatically. So for clients
with javascript capability (the vast majority of your users) the Golf server
will be serving only the initial static page. After that all requests will be
simply redirected to Cloudfront, where Golf has cached the component HTML and
javascript. Only clients interacting in proxy mode will be using local
resources on the Golf server.

###Parallelization of Effort

Typically, websites are developed by small teams of people who can usually
be grouped into the following roles: Graphics, CSS/HTML, Developer, and Backend.
Some teams might lack one or more of the above, or have someone else not listed.
But we think that's generally how it goes.  A common problem with the project
development flow of such a team are what might be called "linear dependencies" -
the Developers can't get to coding until they have a template and set of functional
requirements to work with.  The CSS person can't get to splicing until he has a mockup
to work with.  And the graphics guy isn't done with his mockup until it's approved
by the client.

In Golf, "scaffolding" is an ongoing, iterative, revisioned process.  Developers
can immediately start coding the application logic, and then refine the scaffold
as the project requirements gel.  Templates from the CSS person can be easily
decomposed into components, or the scaffolded product can be styled to look
like the mockup.
