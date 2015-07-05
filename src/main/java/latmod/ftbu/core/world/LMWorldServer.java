package latmod.ftbu.core.world;

import java.util.UUID;

import latmod.ftbu.core.*;
import latmod.ftbu.core.event.LMPlayerEvent;
import latmod.ftbu.core.util.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.*;
import net.minecraftforge.common.util.FakePlayer;

import com.mojang.authlib.GameProfile;

import cpw.mods.fml.relauncher.Side;

public class LMWorldServer extends LMWorld
{
	public final FastMap<Integer, LMPlayerServer> players;
	public final FastMap<String, EntityPos> warps;
	
	public LMWorldServer(UUID id)
	{
		super(Side.SERVER, id);
		players = new FastMap<Integer, LMPlayerServer>();
		warps = new FastMap<String, EntityPos>();
	}
	
	public FastMap<Integer, ? extends LMPlayer> getPlayers()
	{ return players; }
	
	public LMPlayerServer getPlayer(Object o)
	{
		if(o == null || o instanceof FakePlayer) return null;
		else if(o instanceof Integer || o instanceof LMPlayer)
		{
			int h = o.hashCode();
			return (h <= 0) ? null : players.get(h);
		}
		else if(o.getClass() == UUID.class)
		{
			UUID id = (UUID)o;
			
			for(int i = 0; i < players.size(); i++)
			{
				LMPlayerServer p = players.values.get(i);
				if(p.getUUID().equals(id)) return p;
			}
		}
		else if(o instanceof EntityPlayer)
			return getPlayer(((EntityPlayer)o).getUniqueID());
		else if(o instanceof String)
		{
			String s = o.toString();
			
			if(s == null || s.isEmpty()) return null;
			
			for(int i = 0; i < players.size(); i++)
			{
				LMPlayerServer p = players.values.get(i);
				if(p.getName().equalsIgnoreCase(s)) return p;
			}
			
			return getPlayer(LatCoreMC.getUUIDFromString(s));
		}
		
		return null;
	}
	
	public void load(NBTTagCompound tag)
	{
		warps.clear();
		
		NBTTagCompound tagWarps = (NBTTagCompound)tag.getTag("Warps");
		
		if(tagWarps != null && !tagWarps.hasNoTags())
		{
			FastList<String> l = NBTHelper.getMapKeys(tagWarps);
			
			for(int i = 0; i < l.size(); i++)
			{
				int[] a = tagWarps.getIntArray(l.get(i));
				setWarp(l.get(i), a[0], a[1], a[2], a[3]);
			}
		}
	}
	
	public void save(NBTTagCompound tag)
	{
		NBTTagCompound tagWarps = new NBTTagCompound();
		for(int i = 0; i < warps.size(); i++)
			tagWarps.setIntArray(warps.keys.get(i), warps.values.get(i).toIntArray());
		tag.setTag("Warps", tagWarps);
	}
	
	public void writePlayersToNet(NBTTagCompound tag)
	{
		NBTTagList list = new NBTTagList();
		
		for(int i = 0; i < players.values.size(); i++)
		{
			NBTTagCompound tag1 = new NBTTagCompound();
			
			LMPlayerServer p = players.values.get(i);
			p.writeToNet(tag1);
			new LMPlayerEvent.DataSaved(p).post();
			tag1.setLong("MID", p.getUUID().getMostSignificantBits());
			tag1.setLong("LID", p.getUUID().getLeastSignificantBits());
			tag1.setString("N", p.getName());
			tag1.setInteger("PID", p.playerID);
			
			list.appendTag(tag1);
		}
		
		tag.setTag("Players", list);
	}
	
	public void writePlayersToServer(NBTTagCompound tag)
	{
		for(int i = 0; i < players.values.size(); i++)
		{
			NBTTagCompound tag1 = new NBTTagCompound();
			
			LMPlayerServer p = players.values.get(i);
			p.writeToServer(tag1);
			new LMPlayerEvent.DataSaved(p).post();
			tag1.setString("UUID", p.uuidString);
			tag1.setString("Name", p.getName());
			
			tag.setTag(p.playerID + "", tag1);
		}
	}
	
	public void readPlayersFromServer(NBTTagCompound tag)
	{
		players.clear();
		
		FastMap<String, NBTTagCompound> map = NBTHelper.toFastMapWithType(tag);
		
		for(int i = 0; i < map.size(); i++)
		{
			int id = Integer.parseInt(map.keys.get(i));
			NBTTagCompound tag1 = map.values.get(i);
			LMPlayerServer p = new LMPlayerServer(this, id, new GameProfile(LatCoreMC.getUUIDFromString(tag1.getString("UUID")), tag1.getString("Name")));
			p.readFromServer(tag1);
			players.put(p.playerID, p);
		}
		
		for(int i = 0; i < players.values.size(); i++)
			players.values.get(i).onPostLoaded();
	}
	
	// Warps //
	
	public String[] listWarps()
	{ return warps.keys.toArray(new String[0]); }
	
	public EntityPos getWarp(String s)
	{ return warps.get(s); }
	
	public boolean setWarp(String s, int x, int y, int z, int dim)
	{ return warps.put(s, new EntityPos(x + 0.5D, y + 0.5D, z + 0.5D, dim)); }
	
	public boolean remWarp(String s)
	{ return warps.remove(s); }
}