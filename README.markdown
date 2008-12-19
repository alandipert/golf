
Introduction
============

Golf is a web application server. Golf applications form the user interface 
for web services. Golf applications are built with components, which
consist of a HTML template and a corresponding javascript transform. The
template is pure HTML and CSS, and the transform is pure javascript. There
is no serverside component. The javascript replaces dummy content in the
template with actual content, and manages dynamic behaviors between the
elements in the component. Data is passed to and from the web service layer
via JSON or JSONP, with a simple, zero-query, javascript API.

Normally, all this templating and transforming will be done in the browser.
Some clients (like the googlebot web crawler, for example) do not
support javascript and dynamic HTML, however. In this relatively rare, but
absolutely crucial case Golf will use the "headless" browser built into the
Golf application server to render the HTML. This is done transparently, from
the point of view of the application developer and the client. User interface
events are proxied using anchor tags and the page is computed and rendered
in the javascript virtual machine running in the server's headess browser
instance.
