package sdcl.ics.uci.edu.topicLocation.taskToTopics.parser;

// JDK Classes
import java.util.*;

public class Element
{
    private Elements elements;
    private String name;
    private List childNames = new ArrayList();
    private List attributes = new ArrayList();

    public Element (Elements elements, String name)
    {
        this.elements = elements;
        this.name = name;
        
        elements.add (this);
    }

    public void addElement (String name)
    {
        childNames.add (name);
    }

    public void addAttribute (String attribute)
    {
        attributes.add (attribute);
    }

    public String getName()
    {
        return name;
    }

    public void setName (String v)
    {
        name = v;
    }

    public boolean hasChildren()
    {
        return (childNames.size() > 0);
    }

    public List getChildrenNames()
    {
        return childNames;
    }
    
    public List getChildren()
    {
        ArrayList result = new ArrayList();
        for (int i=0; i<childNames.size(); i++)
        {
            result.add (elements.getElement(childNames.get(i).toString()));
        }
        return result;
    }
    

    public List getAttributes()
    {
        return attributes;
    }

}
