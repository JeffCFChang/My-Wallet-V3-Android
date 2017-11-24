package piuk.blockchain.android.ui.shapeshift.overview.adapter

import android.app.Activity
import android.support.annotation.ColorRes
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.text.Spannable
import android.text.style.RelativeSizeSpan
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import info.blockchain.wallet.shapeshift.data.Trade
import kotlinx.android.synthetic.main.item_shapeshift_trade.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.util.DateUtil
import piuk.blockchain.android.util.MonetaryUtil
import piuk.blockchain.android.util.PrefsUtil
import piuk.blockchain.android.util.extensions.getContext
import piuk.blockchain.android.util.extensions.inflate
import java.math.BigDecimal

class TradesDisplayableDelegate<in T>(
        activity: Activity,
        private var btcExchangeRate: Double,
        private var ethExchangeRate: Double,
        private var showCrypto: Boolean,
        private val listClickListener: TradesListClickListener
) : AdapterDelegate<T> {

    private val prefsUtil = PrefsUtil(activity)
    private val monetaryUtil = MonetaryUtil(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC))
    private val dateUtil = DateUtil(activity)

    override fun isForViewType(items: List<T>, position: Int): Boolean =
            items[position] is Trade

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
            TradeViewHolder(parent.inflate(R.layout.item_shapeshift_trade))

    override fun onBindViewHolder(
            items: List<T>,
            position: Int,
            holder: RecyclerView.ViewHolder,
            payloads: List<*>
    ) {

        val viewHolder = holder as TradeViewHolder
        val trade = items[position] as Trade

        if (trade.timestamp > 0) {
            viewHolder.timeSince.text = dateUtil.formatted(trade.timestamp / 1000)
        } else {
            //Existing Web issue - no available date to set
            viewHolder.timeSince.text = ""
        }
        viewHolder.status.text = trade.status.toString()

        viewHolder.result.text = getDisplaySpannable(
                trade.acquiredCoinType,
                trade.quote.withdrawalAmount?: BigDecimal.ZERO
        )

        displayTradeColour(viewHolder, trade)

        viewHolder.result.setOnClickListener {
            showCrypto = !showCrypto
            listClickListener.onValueClicked(showCrypto)
        }

        viewHolder.layout.setOnClickListener {
            listClickListener.onTradeClicked(
                    getRealTradePosition(viewHolder.adapterPosition, items), position
            )
        }
    }

    fun onViewFormatUpdated(isBtc: Boolean, btcFormat: Int) {
        this.showCrypto = isBtc
        monetaryUtil.updateUnit(btcFormat)
    }

    fun onPriceUpdated(btcExchangeRate: Double, ethExchangeRate: Double) {
        this.btcExchangeRate = btcExchangeRate
        this.ethExchangeRate = ethExchangeRate
    }

    private fun getResolvedColor(viewHolder: RecyclerView.ViewHolder, @ColorRes color: Int): Int {
        return ContextCompat.getColor(viewHolder.getContext(), color)
    }

    private fun displayTradeColour(viewHolder: TradeViewHolder, trade: Trade) {

        when (trade.status) {
            Trade.STATUS.COMPLETE -> {
                viewHolder.result.setBackgroundResource(R.drawable.rounded_view_complete)
                viewHolder.status.setTextColor(getResolvedColor(viewHolder, R.color.product_green_medium))
            }
            Trade.STATUS.FAILED -> {
                viewHolder.result.setBackgroundResource(R.drawable.rounded_view_failed)
                viewHolder.status.setTextColor(getResolvedColor(viewHolder, R.color.product_red_medium))
            }
            Trade.STATUS.NO_DEPOSITS -> {
                viewHolder.result.setBackgroundResource(R.drawable.rounded_view_failed)
                viewHolder.status.setTextColor(getResolvedColor(viewHolder, R.color.product_red_medium))
            }
            Trade.STATUS.RECEIVED -> {
                viewHolder.result.setBackgroundResource(R.drawable.rounded_view_inprogress)
                viewHolder.status.setTextColor(getResolvedColor(viewHolder, R.color.product_gray_transferred))
            }
            Trade.STATUS.RESOLVED -> {
                viewHolder.result.setBackgroundResource(R.drawable.rounded_view_green)
                viewHolder.status.setTextColor(getResolvedColor(viewHolder, R.color.product_green_medium))
            }
        }
    }

    //TODO This needs cleaning up.
    private fun getDisplaySpannable(
            cryptoCurrency: String,
            cryptoAmount: BigDecimal
    ): Spannable {
        val spannable: Spannable
        if (showCrypto) {
            val amount = when(cryptoCurrency.toUpperCase()){
                CryptoCurrencies.BTC.symbol -> "${monetaryUtil.getBtcFormat().format(cryptoAmount)} ${getDisplayUnits()}"
                CryptoCurrencies.ETHER.symbol -> "${monetaryUtil.getEthFormat().format(cryptoAmount)} ${getDisplayUnits()}"
                else -> "${cryptoAmount} ${getDisplayUnits()}"//Coin type not specified
            }

            spannable = Spannable.Factory.getInstance().newSpannable(amount)

            spannable.setSpan(
                    RelativeSizeSpan(0.67f),
                    spannable.length - getDisplayUnits().length,
                    spannable.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        } else {

            val fiatBalance = when (cryptoCurrency.toUpperCase()) {
                CryptoCurrencies.BTC.symbol -> cryptoAmount.times(BigDecimal.valueOf(btcExchangeRate))
                CryptoCurrencies.ETHER.symbol -> cryptoAmount.times(BigDecimal.valueOf(ethExchangeRate))
                else -> BigDecimal.ZERO//Coin type not specified
            }

            val fiatString = prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)

            spannable = Spannable.Factory.getInstance().newSpannable(
                    "${monetaryUtil.getFiatFormat(fiatString).format(fiatBalance.abs())} $fiatString")
            spannable.setSpan(
                    RelativeSizeSpan(0.67f),
                    spannable.length - 3,
                    spannable.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        return spannable
    }

    private fun getDisplayUnits(): String =
            monetaryUtil.getBtcUnits()[prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)]

    private fun getRealTradePosition(position: Int, items: List<T>): Int {
        val diff = items.size - items.count { it is Trade }
        return position - diff
    }

    private class TradeViewHolder internal constructor(
            itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        internal var result: TextView = itemView.result
        internal var timeSince: TextView = itemView.date
        internal var status: TextView = itemView.status
        internal var layout: RelativeLayout = itemView.trade_row
    }
}