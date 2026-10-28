/** Legacy save identifiers. Woodland is accepted only to read older profiles;
 * all live scenes now use the Highland campaign. */
public enum Arena {
    WOODLAND, HIGHLANDS;
    public static Arena forWave(int wave) { return wave>=6 ? HIGHLANDS : WOODLAND; }
}
