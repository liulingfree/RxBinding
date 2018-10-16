@file:JvmName("RxView")
@file:JvmMultifileClass

package com.jakewharton.rxbinding2.view

import android.view.KeyEvent
import android.view.View
import android.view.View.OnKeyListener
import androidx.annotation.CheckResult
import com.jakewharton.rxbinding2.internal.PREDICATE_ALWAYS_TRUE
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.MainThreadDisposable
import io.reactivex.functions.Predicate

import com.jakewharton.rxbinding2.internal.checkMainThread

/**
 * Create an observable of key events for `view`.
 *
 * *Warning:* The created observable keeps a strong reference to `view`. Unsubscribe
 * to free this reference.
 * *Warning:* The created observable uses [View.setOnKeyListener] to observe
 * key events. Only one observable can be used for a view at a time.
 *
 * @param handled Predicate invoked each occurrence to determine the return value of the
 * underlying [View.OnKeyListener].
 */
@CheckResult
@JvmOverloads
fun View.keys(handled: Predicate<in KeyEvent> = PREDICATE_ALWAYS_TRUE): Observable<KeyEvent> {
  return ViewKeyObservable(this, handled)
}

private class ViewKeyObservable(
  private val view: View,
  private val handled: Predicate<in KeyEvent>
) : Observable<KeyEvent>() {

  override fun subscribeActual(observer: Observer<in KeyEvent>) {
    if (!checkMainThread(observer)) {
      return
    }
    val listener = Listener(view, handled, observer)
    observer.onSubscribe(listener)
    view.setOnKeyListener(listener)
  }

  private class Listener(
    private val view: View,
    private val handled: Predicate<in KeyEvent>,
    private val observer: Observer<in KeyEvent>
  ) : MainThreadDisposable(), OnKeyListener {

    override fun onKey(v: View, keyCode: Int, event: KeyEvent): Boolean {
      if (!isDisposed) {
        try {
          if (handled.test(event)) {
            observer.onNext(event)
            return true
          }
        } catch (e: Exception) {
          observer.onError(e)
          dispose()
        }

      }
      return false
    }

    override fun onDispose() {
      view.setOnKeyListener(null)
    }
  }
}