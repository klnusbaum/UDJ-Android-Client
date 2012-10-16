package org.klnusbaum.udj;

import org.klnusbaum.udj.containers.StringIdable;
import android.widget.BaseAdapter;

import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;



public abstract class StringIdableAdapter<T extends StringIdable> extends BaseAdapter{
  private ConcurrentMap<String, Long> idMap;
  private long currentAvailableMapId;
  private List<T> itemList;

  public StringIdableAdapter(List<T> items){
    this.itemList = items;
    this.currentAvailableMapId = 0;
    idMap = new ConcurrentHashMap<String, Long>();
    if(items != null){
      for(StringIdable st: itemList){
        idMap.put(st.getId(), currentAvailableMapId);
        currentAvailableMapId++;
      }
    }
  }

  public int getCount(){
    if(itemList != null){
      return itemList.size();
    }
    return 0;
  }



  public Object getItem(int position){
    if(itemList != null){
      return itemList.get(position);
    }
    return null;
  }

  public long getItemId(int position){
    return idMap.get(itemList.get(position).getId());
  }

  public void addItem(int position, T item){
    if(!idMap.keySet().contains(item.getId())){
        idMap.put(item.getId(), currentAvailableMapId);
        currentAvailableMapId++;
    }
    itemList.add(position, item);
    notifyDataSetChanged();
  }

  public synchronized void removeItem(int position){
    idMap.remove(((StringIdable)getItem(position)).getId());
    itemList.remove(position);
    notifyDataSetChanged();
  }

  public synchronized void removeItem(T toRemove){
    itemList.remove(toRemove);
    idMap.remove(toRemove.getId());
    notifyDataSetChanged();
  }

  public boolean isEmpty(){
    return itemList == null || itemList.isEmpty();
  }

  private synchronized boolean listContainsId(String s){
    for(StringIdable st: itemList){
      if(s.equals(st.getId())){
        return true;
      }
    }
    return false;
  }

  public synchronized void updateList(List<T> newItems){
    for(T item: newItems){
      if (!idMap.keySet().contains(item.getId())){
        idMap.put(item.getId(), currentAvailableMapId);
        currentAvailableMapId++;
      }
    }
    this.itemList = newItems;
    /**
     * This algorithm is n*m complexity, might be able to do better...
     */
    for(String s: idMap.keySet()){
      if(!listContainsId(s)){
        idMap.remove(s);
      }
    }
    notifyDataSetChanged();
  }
}
