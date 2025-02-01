import pdfplumber as plumber

pdf1 = plumber.open("1.pdf")
page = pdf1.pages
print(page[0].chars[4])

