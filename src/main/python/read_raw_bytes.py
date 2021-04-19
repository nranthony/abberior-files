
def get_bytes_from_file(filename):
    return open(filename, "rb").read()

#fname = r"C:\temp-data\abberior_obf_examples\Sample 7.obf"
fname = r"C:\\ici-cloud-sections\\WBRB Abberior STED\\2021\\Neil\\2021-03-17\\Ab4C_02.obf"
fbytes = get_bytes_from_file(fname)

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

    
