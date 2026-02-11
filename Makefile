.PHONY: snippets fmt clean build dist upload run-example

fmt:
	sbt scalafmtAll

clean:
	-find -name target -exec rm -rf \{\} \;
	sbt clean

build:
	sbt compile

test:
	sbt test

dist: clean build test snippets
	publish-sbt-sonatype publishSigned

publish-local: dist
	sbt publishLocal

publish: dist run-example
	publish-sbt-sonatype sonaUpload

snippets:
	sbt updateVersionInDocs
	./script/update_snippets.py .

run-example: publish-local
	cd example && scala-cli run .
