/**
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package scalding

object TestData {

	import java.io.File
	
  def tempFile(): File = {
    val file = File.createTempFile("word-count-scalding-spec", ".txt")
    file.deleteOnExit()
    file
  }

  val activatorText = """
Typesafe Activator is a browser-based or command-line tool that helps developers get started with the Typesafe Reactive Platform.

A new addition to the Typesafe Reactive Platform is Typesafe Activator, a unique, browser-based tool that helps developers get started with Typesafe technologies quickly and easily. Activator is a hub for developers wanting to build Reactive applications. Unlike previous developer-focused offerings that are delivered simply via a website, Activator breaks new ground by delivering a rich application directly to the desktop. Activator updates in real-time with new content from Typesafe and value-add third parties, helping developers engage and adopt Typesafe technologies in an entirely frictionless manner.

Getting started is a snap; just download, extract and run the executable to start building applications immediately via the easy to use wizard-based interface. Common development patterns are presented through reusable templates that are linked to in-context tutorials, which explain step-by-step exactly how things work. The Activator environment supports each stage of the application development lifecycle: Code, Compile, Test and Run. At the appropriate time, Activator can generate fully fledged projects for the leading IDEs so that application development can continue in these environments.

The rich developer content in Typesafe Activator is dynamic and customizable. New templates are published regularly on the Typesafe website, and anyone can contribute new templates!"""

}