/*
 * Copyright (c) 2011-2013 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 *     The Eclipse Public License is available at
 *     http://www.eclipse.org/legal/epl-v10.html
 *
 *     The Apache License v2.0 is available at
 *     http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */

package io.vertx.core.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

class FutureImpl<T> implements Future<T>, Handler<AsyncResult<T>> {
  private boolean failed;
  private boolean succeeded;
  private Handler<AsyncResult<T>> handler;
  private T result;
  private Throwable throwable;

  /**
   * Create a future that hasn't completed yet
   */
  FutureImpl() {
  }

  /**
   * The result of the operation. This will be null if the operation failed.
   */
  public T result() {
    return result;
  }

  /**
   * An exception describing failure. This will be null if the operation succeeded.
   */
  public Throwable cause() {
    return throwable;
  }

  /**
   * Did it succeeed?
   */
  public synchronized boolean succeeded() {
    return succeeded;
  }

  /**
   * Did it fail?
   */
  public synchronized boolean failed() {
    return failed;
  }

  /**
   * Has it completed?
   */
  public synchronized boolean isComplete() {
    return failed || succeeded;
  }

  /**
   * Set a handler for the result. It will get called when it's complete
   */
  public Future<T> setHandler(Handler<AsyncResult<T>> handler) {
    boolean callHandler;
    synchronized (this) {
      this.handler = handler;
      callHandler = isComplete();
    }
    if (callHandler) {
      handler.handle(this);
    }
    return this;
  }

  @Override
  public void complete(T result) {
    if (!tryComplete(result)) {
      throw new IllegalStateException("Result is already complete: " + (succeeded ? "succeeded" : "failed"));
    }
  }

  @Override
  public void complete() {
    if (!tryComplete()) {
      throw new IllegalStateException("Result is already complete: " + (succeeded ? "succeeded" : "failed"));
    }
  }

  @Override
  public void fail(Throwable cause) {
    if (!tryFail(cause)) {
      throw new IllegalStateException("Result is already complete: " + (succeeded ? "succeeded" : "failed"));
    }
  }

  @Override
  public void fail(String failureMessage) {
    if (!tryFail(failureMessage)) {
      throw new IllegalStateException("Result is already complete: " + (succeeded ? "succeeded" : "failed"));
    }
  }

  @Override
  public boolean tryComplete(T result) {
    Handler<AsyncResult<T>> h;
    synchronized (this) {
      if (succeeded || failed) {
        return false;
      }
      this.result = result;
      succeeded = true;
      h = handler;
    }
    if (h != null) {
      h.handle(this);
    }
    return true;
  }

  @Override
  public boolean tryComplete() {
    return tryComplete(null);
  }

  public void handle(Future<T> ar) {
    if (ar.succeeded()) {
      complete(ar.result());
    } else {
      fail(ar.cause());
    }
  }

  @Override
  public Handler<AsyncResult<T>> completer() {
    return this;
  }

  @Override
  public void handle(AsyncResult<T> asyncResult) {
    if (asyncResult.succeeded()) {
      complete(asyncResult.result());
    } else {
      fail(asyncResult.cause());
    }
  }

  @Override
  public boolean tryFail(Throwable cause) {
    Handler<AsyncResult<T>> h;
    synchronized (this) {
      if (succeeded || failed) {
        return false;
      }
      this.throwable = cause != null ? cause : new NoStackTraceThrowable(null);
      failed = true;
      h = handler;
    }
    if (h != null) {
      h.handle(this);
    }
    return true;
  }

  @Override
  public boolean tryFail(String failureMessage) {
    return tryFail(new NoStackTraceThrowable(failureMessage));
  }

  @Override
  public String toString() {
    synchronized (this) {
      if (succeeded) {
        return "Future{result=" + result + "}";
      }
      if (failed) {
        return "Future{cause=" + throwable.getMessage() + "}";
      }
      return "Future{unresolved}";
    }
  }
}
