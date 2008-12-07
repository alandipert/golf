require 'rubygems'
require 'mechanize'

a = WWW::Mechanize.new

a.get('http://www.golf.com/demo') do |page|
  
  loop do
    page.links[0].click
  end

end
