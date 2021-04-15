

def get_bytes_from_file(filename):
    return open(filename, "rb").read()

fname = r"C:\temp-data\abberior_obf_examples\Sample 7.obf"
fbytes = get_bytes_from_file(fname)

print(fbytes[-64:])

i = 5
with open(fname, 'rb') as f:
    
    looking = True
    
    while looking:
        i += 1
        f.seek(-i,2)
        chkstr = f.read(5)
        if "<?xml" in chkstr.decode("utf-8"):
            looking = False

    f.seek(-i,2)
    xml = f.read().decode("utf-8")
        
print(f"{chkstr} at byte {i}")


        
        > f = open(r'C:\test.txt')
>>> f.seek(-3, 2) # move to the 3rd character from the end of the file
>>> f.read()
    
