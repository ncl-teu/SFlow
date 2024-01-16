package net.gripps.ccn.core;

/**
 * Created by Hidehiro Kanemitsu on 2018/12/07.
 */
public class ContentsPiece implements Cloneable {

    /**
     * キャッシュかどうか
     */
    boolean isCache;
    /**
     * コンテンツID(UUID)
     */
    private String  contentID;

    /**
     * カスタマイズされたID
     */
    private long customID;

    /**
     * コンテンツ名(パス区切り）
     */
    private String prefix;

    /**
     * シーケンス番号(1から開始）
     */
    long sequenceID;

    /**
     * 最大のシーケンス番号
     */
    long maxID;

    long size;
}
