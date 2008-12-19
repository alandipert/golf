
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
