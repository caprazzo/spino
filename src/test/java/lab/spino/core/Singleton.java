package lab.spino.core;

/**
 * Created with IntelliJ IDEA.
 * User: caprazzo
 * Date: 19/04/2013
 * Time: 15:41
 * To change this template use File | Settings | File Templates.
 */
public final class Singleton {

    private enum Holder {
        INSTANCE;
        private static Impl impl = new Impl();
    }

    public void start() {
        Holder.INSTANCE.impl.start();
    }

    private static class Impl {
        public void start() {

        }
    }

}
