
package xyz.neilanthony;


abstract class BFContainer {
    private String idStr = "";
    private String name = "";
    private int idIndex = -1;
    private int bfIndex = -1;
    private String containerType = "";
    
    BFContainer (String type) {
        this.containerType = type;
    }
    
    
    public String getIDString() {
        return this.idStr;
    }
    public String getName() {
        return this.name;
    }
    public int getIDIndex() {
        return this.idIndex;
    }
    public int getBDIndex() {
        return this.bfIndex;
    }
    
    // setting the string of the index sets the other
    public void setIDString(String idStr) {
        this.idStr = idStr;
        String replaceStr = String.format("%s:",containerType);
        this.idIndex = (int)Integer.valueOf(idStr.replace(replaceStr, ""));
    }
    public void setIDIndex(int idIdx) {
        this.idIndex = idIdx;
        this.idStr = String.format("%s:%d", this.containerType, idIdx);
    }
    public void setName(String nameStr) {
        this.name = nameStr;
    }
    public void setBFIndex(int bfIdx) {
        this.bfIndex = bfIdx;
    }
}
