import React from 'react';
import styled from 'styled-components';
import {useTheme} from '../hooks/useTheme';
import {ThemeName} from '../types';

const HeaderContainer = styled.header`
    background-color: ${({theme}) => theme.colors.surface};
    padding: 1rem;
    border-bottom: 1px solid ${(props) => props.theme.colors.border};
`;

const ThemeSelect = styled.select`
    padding: 0.5rem;
    border-radius: ${(props) => props.theme.sizing.borderRadius.sm};
    border: 1px solid ${(props) => props.theme.colors.border};
`;

interface HeaderProps {
    onThemeChange: (theme: ThemeName) => void;
}

const Header: React.FC<HeaderProps> = ({onThemeChange}) => {
    const [currentTheme, setTheme] = useTheme();
    React.useEffect(() => {
        console.log('Header mounted with theme:', currentTheme);
        return () => {
            console.log('Header unmounting');
        };
    }, []);
    React.useEffect(() => {
        console.log('Theme changed to:', currentTheme);
    }, [currentTheme]);
    const handleThemeChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
        const newTheme = e.target.value as ThemeName;
        console.log('Theme selection changed:', newTheme);
        setTheme(newTheme);
    };


    return (
        <HeaderContainer>
            <ThemeSelect value={currentTheme} onChange={handleThemeChange}>
                <option value="main">Day</option>
                <option value="night">Night</option>
                <option value="forest">Forest</option>
                <option value="pony">Bubblegum</option>
                <option value="alien">Alien</option>
            </ThemeSelect>
        </HeaderContainer>
    );
};
// Log when the component is defined
console.log('Header component defined');


export default Header;