package sdcl.ics.uci.edu.topicLocation.taskToTopics.parser;

// JDK Classes
import java.util.*;

public class Elements
{
    private List elements = new ArrayList();
    private Map elementsNamed = new HashMap();
    
    public void add (Element element)
    {
        elements.add (element);
        elementsNamed.put (element.getName(), element);
    }
    
    public Element getRootElement ()
    {
        return (Element)elements.get(0);
    }
    
    public Element getElement (String name)
    {
        return (Element)elementsNamed.get(name);
    }
    
    public int size()
    {
        return elements.size();
    }
    
    public Element get (int id)
    {
        return (Element)elements.get(id);
    }
    
}
