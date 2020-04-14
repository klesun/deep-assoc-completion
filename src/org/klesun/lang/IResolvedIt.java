package org.klesun.lang;

/**
 * unlike IReusableIt, this one does not include MemIt - supposedly all iterables implementing this
 * interface have any of values available at constant time: either first, second, etc or last
 *
 * basically it's for collections implementing IIt
 */
public interface IResolvedIt<T> extends IReusableIt<T>
{
    public static <Ts> IResolvedIt<Ts> fst(Lang.S<IResolvedIt<Ts>>... attempts)
    {
        for (Lang.S<IResolvedIt<Ts>> a: attempts) {
            IResolvedIt<Ts> result = a.get();
            if (result.has()) {
                return result;
            }
        }
        return L.non();
    }
}
