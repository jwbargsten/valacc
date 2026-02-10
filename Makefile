.PHONY: snippets fmt clean build dist upload

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

local: dist
	sbt publishLocal
upload: dist
	publish-sbt-sonatype sonaUpload

snippets:
	./script/update_snippets.py .

run-example:
	cd example && scala-cli run .
