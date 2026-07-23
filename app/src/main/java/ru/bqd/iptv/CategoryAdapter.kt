package ru.bqd.iptv

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.TextView

data class CatItem(val title: String, val count: Int, val type: String, val group: String? = null)

class CategoryAdapter(
    private val ctx: Context,
    private var items: List<CatItem>,
    private val compact: Boolean = false
) : BaseAdapter() {

    companion object {
        private const val TYPE_ROW = 0
        private const val TYPE_PLSEL = 1
    }

    var activePos: Int = -1
        set(v) { if (field != v) { field = v; notifyDataSetChanged() } }

    fun update(list: List<CatItem>) { items = list; notifyDataSetChanged() }

    override fun getCount() = items.size
    override fun getItem(p: Int) = items[p]
    override fun getItemId(p: Int) = p.toLong()

    override fun getViewTypeCount() = 2

    override fun getItemViewType(position: Int): Int =
        if (!compact && items[position].type == "PLSEL") TYPE_PLSEL else TYPE_ROW

    private fun iconName(item: CatItem): String = when (item.type) {
        "SETTINGS" -> "settings"
        "SEARCH" -> "search"
        "FAV" -> "star"
        "PLSEL" -> "playlist_play"
        "ALL" -> "apps"
        else -> GroupIcons.iconFor(item.group ?: item.title)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val item = items[position]
        return if (getItemViewType(position) == TYPE_PLSEL) {
            plSelView(item, convertView, parent)
        } else {
            rowView(item, position, convertView, parent)
        }
    }

    private fun plSelView(item: CatItem, convertView: View?, parent: ViewGroup?): View {
        val v = convertView
            ?: LayoutInflater.from(ctx).inflate(R.layout.item_playlist_sel, parent, false)
        v.findViewById<TextView>(R.id.plSelName).text = item.title
        return v
    }

    private fun rowView(item: CatItem, position: Int, convertView: View?, parent: ViewGroup?): View {
        val v = convertView
            ?: LayoutInflater.from(ctx).inflate(R.layout.item_category, parent, false)
        val icon = v.findViewById<TextView>(R.id.catIcon)
        val left = v.findViewById<TextView>(R.id.catName)
        val right = v.findViewById<TextView>(R.id.catCount)

        IconFont.apply(icon, iconName(item))
        left.text = item.title

        if (compact) {
            left.visibility = View.GONE
            right.visibility = View.GONE
            icon.layoutParams = (icon.layoutParams as LinearLayout.LayoutParams).apply {
                width = LinearLayout.LayoutParams.MATCH_PARENT
                marginEnd = 0
            }
            val pad = (ctx.resources.displayMetrics.density * 12).toInt()
            v.setPadding(0, pad, 0, pad)
            if (position == activePos) v.setBackgroundResource(R.drawable.rail_active)
            else v.setBackgroundResource(0)
        } else {
            left.visibility = View.VISIBLE
            if (item.count > 0) {
                right.text = item.count.toString()
                right.visibility = View.VISIBLE
            } else {
                right.text = ""
                right.visibility = View.GONE
            }
        }
        return v
    }
}
