#!/usr/bin/python3

import mimetypes, os, sys

# print(mimetypes.guess_type("index.html"))
test_file = sys.argv[1]
print(mimetypes.MimeTypes().guess_type(test_file)[0])
