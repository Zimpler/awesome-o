uberjar:
	lein uberjar

deploy: uberjar
	aws lambda update-function-code --function-name "awesomeo_announcements" --zip-file "fileb://$(CURDIR)/target/awesome-o-standalone.jar" --publish
	aws lambda update-function-code --function-name "awesomeo_mention"       --zip-file "fileb://$(CURDIR)/target/awesome-o-standalone.jar" --publish
	aws lambda update-function-code --function-name "awesomeo_scheduled"     --zip-file "fileb://$(CURDIR)/target/awesome-o-standalone.jar" --publish
